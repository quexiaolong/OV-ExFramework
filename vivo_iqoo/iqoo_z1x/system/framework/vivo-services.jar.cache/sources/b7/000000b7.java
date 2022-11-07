package com.android.server.am;

import android.os.SystemClock;
import android.os.UserHandle;
import com.vivo.common.utils.VLog;
import com.vivo.services.rms.ProcessList;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class FrozenPackageRecord {
    static final String TAG = "VFPS";
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    int appId;
    int flag;
    String frozenReason;
    String packageName;
    String stringName;
    int tryFrozenCnt;
    int uid;
    int userId;
    long frozenTime = 0;
    long frozenRealTime = 0;
    long frozenUptime = 0;
    long startFrozenTime = 0;
    long startUnfrozenTime = 0;
    int status = 0;
    String unfrozenReason = "unknow";
    private ArrayList<ProcessRecord> mNormalProcs = new ArrayList<>();
    private ArrayList<NativeProcessRecord> mNativeProcs = new ArrayList<>();
    private ArrayList<ConnectionRecord> mServices = new ArrayList<>();

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static final class NativeProcessRecord {
        String packageName;
        int pid;
        int ppid;
        String processName = null;
        String shortStringName;
        String stringName;
        int uid;
        int userId;

        /* JADX INFO: Access modifiers changed from: package-private */
        public NativeProcessRecord(String packageName, int pid, int ppid, int uid, int userId) {
            this.uid = -1;
            this.pid = -1;
            this.ppid = -1;
            this.userId = -1;
            this.packageName = null;
            this.uid = uid;
            this.pid = pid;
            this.ppid = ppid;
            this.userId = userId;
            this.packageName = packageName;
        }

        public void setProcessName(String processName) {
            this.processName = processName;
        }

        public String toShortString() {
            String str = this.shortStringName;
            if (str != null) {
                return str;
            }
            StringBuilder sb = new StringBuilder(128);
            toShortString(sb);
            String sb2 = sb.toString();
            this.shortStringName = sb2;
            return sb2;
        }

        public String toShortString(StringBuilder sb) {
            sb.append(this.pid);
            sb.append(':');
            sb.append(this.processName);
            sb.append('/');
            int i = this.uid;
            if (i < 10000) {
                sb.append(i);
            } else {
                sb.append('u');
                sb.append(this.userId);
                int appId = UserHandle.getAppId(this.uid);
                if (appId >= 10000) {
                    sb.append('a');
                    sb.append(appId + ProcessList.INVALID_ADJ);
                } else {
                    sb.append('s');
                    sb.append(appId);
                }
            }
            return sb.toString();
        }

        public String toString() {
            String str = this.stringName;
            if (str != null) {
                return str;
            }
            StringBuilder sb = new StringBuilder(128);
            sb.append("NativeProcessRecord{");
            sb.append(Integer.toHexString(System.identityHashCode(this)));
            sb.append(' ');
            toShortString(sb);
            sb.append('}');
            String sb2 = sb.toString();
            this.stringName = sb2;
            return sb2;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public FrozenPackageRecord(int uid, String packageName, String reason, int flag) {
        this.uid = -1;
        this.appId = -1;
        this.userId = -1;
        this.packageName = null;
        this.frozenReason = "unknow";
        this.packageName = packageName;
        this.uid = uid;
        this.frozenReason = reason;
        this.flag = flag;
        this.appId = UserHandle.getAppId(uid);
        this.userId = UserHandle.getUserId(uid);
    }

    public boolean addNormalProcs(ProcessRecord proc) {
        if (proc == null) {
            return false;
        }
        this.mNormalProcs.add(proc);
        return true;
    }

    public boolean rmNormalProcs(ProcessRecord proc) {
        ArrayList<ProcessRecord> arrayList;
        if (proc == null || (arrayList = this.mNormalProcs) == null || arrayList.size() == 0) {
            return false;
        }
        if (this.mNormalProcs.contains(proc)) {
            this.mNormalProcs.remove(proc);
            return true;
        }
        return true;
    }

    public boolean addNativeProcs(NativeProcessRecord proc) {
        if (proc == null) {
            return false;
        }
        this.mNativeProcs.add(proc);
        return true;
    }

    public boolean rmNativeProcs(NativeProcessRecord proc) {
        ArrayList<NativeProcessRecord> arrayList;
        if (proc == null || (arrayList = this.mNativeProcs) == null || arrayList.size() == 0) {
            return false;
        }
        if (this.mNativeProcs.contains(proc)) {
            this.mNativeProcs.remove(proc);
            return true;
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ProcessRecord getNormalProcessFromPid(int pid) {
        ArrayList<ProcessRecord> arrayList = this.mNormalProcs;
        if (arrayList == null || arrayList.size() == 0) {
            return null;
        }
        Iterator<ProcessRecord> it = this.mNormalProcs.iterator();
        while (it.hasNext()) {
            ProcessRecord proc = it.next();
            if (proc.pid == pid) {
                return proc;
            }
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public NativeProcessRecord getNativeProcessFromPid(int pid) {
        ArrayList<NativeProcessRecord> arrayList = this.mNativeProcs;
        if (arrayList == null || arrayList.size() == 0) {
            return null;
        }
        Iterator<NativeProcessRecord> it = this.mNativeProcs.iterator();
        while (it.hasNext()) {
            NativeProcessRecord proc = it.next();
            if (proc.pid == pid) {
                return proc;
            }
        }
        return null;
    }

    public boolean addNormalProcs(ArrayList<ProcessRecord> procs) {
        if (procs == null || procs.size() == 0) {
            return false;
        }
        this.mNormalProcs.clear();
        this.mNormalProcs.addAll(procs);
        return true;
    }

    public boolean addNativeProcs(ArrayList<NativeProcessRecord> procs) {
        if (procs == null || procs.size() == 0) {
            return false;
        }
        this.mNativeProcs.clear();
        this.mNativeProcs.addAll(procs);
        return true;
    }

    public boolean addServices(ArrayList<ConnectionRecord> servs) {
        if (servs == null || servs.size() == 0) {
            return false;
        }
        this.mServices.addAll(servs);
        return true;
    }

    public ArrayList<ProcessRecord> getNormalProcesses() {
        return this.mNormalProcs;
    }

    public ArrayList<NativeProcessRecord> getNativeProcesses() {
        return this.mNativeProcs;
    }

    public ArrayList<ConnectionRecord> getServiceList() {
        return this.mServices;
    }

    public String getFrozenTime() {
        if (this.frozenTime > 0) {
            return TIME_FORMAT.format(new Date(this.frozenTime));
        }
        return "unknow";
    }

    public String getFrozenInterval() {
        if (this.frozenRealTime > 0) {
            long now = SystemClock.elapsedRealtime();
            long interval = now - this.frozenRealTime;
            if (interval > 0) {
                long day = interval / 86400000;
                long dayExtra = interval % 86400000;
                long hour = dayExtra / 3600000;
                long hourExtra = dayExtra % 3600000;
                long min = hourExtra / 60000;
                long minExtra = hourExtra % 60000;
                long sec = minExtra / 1000;
                return "interval=" + day + "D" + hour + "H" + min + "M" + sec + "S";
            }
            return "unknow";
        }
        return "unknow";
    }

    public long getFrozenUptimeInterval() {
        if (this.frozenUptime > 0) {
            long now = SystemClock.uptimeMillis();
            long interval = now - this.frozenUptime;
            return interval;
        }
        return 0L;
    }

    public long getSpendFrozenTime() {
        if (this.startFrozenTime > 0) {
            long now = SystemClock.elapsedRealtime();
            long interval = now - this.startFrozenTime;
            return interval;
        }
        return 0L;
    }

    public long getSpendUnfrozenTime() {
        if (this.startUnfrozenTime > 0) {
            long now = SystemClock.elapsedRealtime();
            long interval = now - this.startUnfrozenTime;
            return interval;
        }
        return 0L;
    }

    public int frozenFromWhich(String reason) {
        if (reason == null) {
            VLog.d("VFPS", "frozen reason is unknown !");
            return 99;
        } else if (reason.contains("pem")) {
            return 0;
        } else {
            if (reason.contains("rms")) {
                return 1;
            }
            if (!reason.contains("quickfrozen")) {
                return 99;
            }
            return 2;
        }
    }

    public int unfrozenByWhich(String reason) {
        if (reason == null) {
            VLog.d("VFPS", "unfrozen reason is unknown !");
            return 99;
        } else if (reason.contains("audio focus")) {
            return 0;
        } else {
            if (reason.contains("start activity")) {
                return 1;
            }
            if (reason.contains("add widget")) {
                return 2;
            }
            if (reason.contains("media resource")) {
                return 3;
            }
            if (reason.contains("process died")) {
                return 4;
            }
            if (reason.contains("bind ser")) {
                return 5;
            }
            if (reason.contains("phone state")) {
                return 6;
            }
            if (reason.contains("media key code")) {
                return 7;
            }
            if (reason.contains("push service")) {
                return 8;
            }
            if (reason.contains("notification")) {
                return 9;
            }
            if (reason.contains("dump anr")) {
                return 10;
            }
            if (reason.contains("window drawn timeout")) {
                return 11;
            }
            if (reason.contains("sync binder")) {
                return 12;
            }
            if (reason.contains("kill service")) {
                return 13;
            }
            if (reason.contains("stop bg service")) {
                return 14;
            }
            if (!reason.contains("unbind service")) {
                return 99;
            }
            return 15;
        }
    }

    public String toString() {
        String str = this.stringName;
        if (str != null) {
            return str;
        }
        StringBuilder sb = new StringBuilder(128);
        sb.append("FrozenPackageRecord{");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(' ');
        sb.append("u" + this.userId);
        sb.append(' ');
        sb.append(this.uid);
        sb.append(' ');
        sb.append(this.packageName);
        sb.append('}');
        String sb2 = sb.toString();
        this.stringName = sb2;
        return sb2;
    }
}