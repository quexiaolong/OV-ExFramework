package com.android.server.am.anr;

import android.content.Context;
import android.os.Debug;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import com.android.internal.util.DumpUtils;
import com.android.server.am.ActivityManagerService;
import com.vivo.app.anr.IANRManager;
import com.vivo.face.common.data.Constants;
import java.io.File;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.ListIterator;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class ANRManagerService extends IANRManager.Stub {
    private static final boolean DEBUG = true;
    private static final int DURATION_DUMP_PROCESS_TRACE = 5000;
    private static final int MAX_LINE = 10;
    private static final int MAX_PROCESS_TRACE_COUNT = 3;
    private static final String TAG = "ANRManager";
    private Context mContext;
    private Handler mHandler;
    private final ActivityManagerService mService;
    public static int[] mZygotePids = null;
    public static volatile ArrayList<File> dumpProcessFiles = new ArrayList<>();
    private final SparseArray<ArrayList<String>> mLogs = new SparseArray<>();
    private SimpleDateFormat mFormat = new SimpleDateFormat("MM-dd HH:mm:ss");

    public void dummy() {
    }

    public ANRManagerService(Context context, ActivityManagerService service) {
        this.mService = service;
        this.mContext = context;
        HandlerThread thread = new HandlerThread("ANRManagerService");
        thread.start();
        Handler handler = new Handler(thread.getLooper());
        this.mHandler = handler;
        handler.post(new Runnable() { // from class: com.android.server.am.anr.-$$Lambda$ANRManagerService$TaD3Mw6_hzeFJrQHAlCF3hy_0og
            @Override // java.lang.Runnable
            public final void run() {
                ANRManagerService.this.lambda$new$0$ANRManagerService();
            }
        });
    }

    public void appLooperBlocked(String processName, final int pid, String msg, final int totaltime, final String binderCall) throws RemoteException {
        synchronized (this.mLogs) {
            insertLogsLocked(processName, pid, msg);
            if (totaltime == 5000 || (totaltime > 5000 && totaltime <= 15000 && totaltime % 5000 == 0)) {
                this.mHandler.post(new Runnable() { // from class: com.android.server.am.anr.-$$Lambda$ANRManagerService$3PIEjRdz3GMBgFMqG_hB-TzmG-I
                    @Override // java.lang.Runnable
                    public final void run() {
                        ANRManagerService.this.lambda$appLooperBlocked$1$ANRManagerService(totaltime, pid, binderCall);
                    }
                });
            }
        }
    }

    public /* synthetic */ void lambda$appLooperBlocked$1$ANRManagerService(int totaltime, int pid, String binderCall) {
        VSlog.d(TAG, "totaltime = " + totaltime);
        if (Process.myPid() == pid) {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            boolean isBlockUntilThreadAvailable = false;
            int length = stackTrace.length;
            int i = 0;
            while (true) {
                if (i >= length) {
                    break;
                }
                StackTraceElement element = stackTrace[i];
                if (!element.toString().contains("android.os.Binder.blockUntilThreadAvailable()")) {
                    i++;
                } else {
                    isBlockUntilThreadAvailable = true;
                    break;
                }
            }
            if (isBlockUntilThreadAvailable) {
                VSlog.d(TAG, "isBlockUntilThreadAvailable = " + isBlockUntilThreadAvailable);
                dumpProcessTrace(pid);
                return;
            }
            return;
        }
        dumpProcessTrace(pid);
        if (!TextUtils.isEmpty(binderCall)) {
            VSlog.d(TAG, "binderCall = " + binderCall);
            int binderPid = this.mService.getBinderTargetPID(binderCall);
            if (binderPid != -1 && binderPid != pid) {
                dumpProcessTrace(binderPid);
            }
        }
    }

    public void appBinderTimeout(String processName, int pid, String service, int totaltime) throws RemoteException {
        synchronized (this.mLogs) {
            insertLogsLocked(processName, pid, "Binder time out, service = " + service);
        }
    }

    public String getAllMessages(int pid) throws RemoteException {
        String logsLocked;
        synchronized (this.mLogs) {
            logsLocked = getLogsLocked(pid);
        }
        return logsLocked;
    }

    public String[] getAllBinders(int pid) throws RemoteException {
        return null;
    }

    public void clearAllMessages(final int pid) throws RemoteException {
        synchronized (this.mLogs) {
            deleteLogsLocked(pid);
        }
        this.mHandler.post(new Runnable() { // from class: com.android.server.am.anr.-$$Lambda$ANRManagerService$_8LVAJNr4zDYLjrnfOp0eoh3Ac4
            @Override // java.lang.Runnable
            public final void run() {
                ANRManagerService.this.lambda$clearAllMessages$2$ANRManagerService(pid);
            }
        });
    }

    private String getAnrTracesDir() {
        String tracesDir = SystemProperties.get("dalvik.vm.stack-trace-dir", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
        if (tracesDir.isEmpty()) {
            String tracesPath = SystemProperties.get("dalvik.vm.stack-trace-file", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
            if (tracesPath.isEmpty()) {
                VSlog.w(TAG, "getAnrTracesDir: no trace path configured");
                return null;
            }
            File tracesFile = new File(tracesPath);
            File anrDir = new File(tracesFile.getParent() + File.separator + "anrmanager");
            if (!anrDir.exists()) {
                if (!anrDir.mkdirs()) {
                    return null;
                }
                String anrDirPath = anrDir.getAbsolutePath();
                return anrDirPath;
            }
            String anrDirPath2 = anrDir.getAbsolutePath();
            return anrDirPath2;
        }
        File anrDir2 = new File(tracesDir + File.separator + "anrmanager");
        if (!anrDir2.exists()) {
            if (!anrDir2.mkdirs()) {
                return null;
            }
            String anrDirPath3 = anrDir2.getAbsolutePath();
            return anrDirPath3;
        }
        String anrDirPath4 = anrDir2.getAbsolutePath();
        return anrDirPath4;
    }

    private void dumpProcessTrace(int pid) {
        String tracesDir = getAnrTracesDir();
        if (tracesDir == null || tracesDir.length() == 0 || pid == -1) {
            return;
        }
        String tracesPath = tracesDir + File.separator + "trace.txt";
        File tracesFile = new File(tracesPath);
        try {
            long sTime = SystemClock.elapsedRealtime();
            if (isJavaProcess(pid)) {
                Debug.dumpJavaBacktraceToFileTimeout(pid, tracesPath, 2);
                VSlog.d(TAG, "Done with pid " + pid + " in " + (SystemClock.elapsedRealtime() - sTime) + "ms");
            } else {
                long sTime2 = SystemClock.elapsedRealtime();
                Debug.dumpNativeBacktraceToFileTimeout(pid, tracesPath, 2);
                VSlog.d(TAG, "Done with native pid " + pid + " in " + (SystemClock.elapsedRealtime() - sTime2) + "ms");
            }
            int lastPos = tracesPath.lastIndexOf(".");
            String backupPath = tracesPath.substring(0, lastPos) + "_" + pid + "_" + new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS").format(new Date(System.currentTimeMillis())) + tracesPath.substring(lastPos);
            File destFile = new File(backupPath);
            boolean rename = tracesFile.renameTo(destFile);
            if (rename) {
                dumpProcessFiles.add(destFile);
                VSlog.d(TAG, "dumpProcessFiles add: " + destFile);
            }
        } catch (Exception ioe) {
            VSlog.w(TAG, "Exception dump process traces :", ioe);
        }
        try {
            clearRedundantTraces(pid);
        } catch (Exception ioe2) {
            VSlog.w(TAG, "Exception clear redundant traces :", ioe2);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: clearCachedTraces */
    public void lambda$clearAllMessages$2$ANRManagerService(int pid) {
        try {
            String tracesDirPath = getAnrTracesDir();
            if (tracesDirPath != null && tracesDirPath.length() != 0) {
                File tracesFileDir = new File(tracesDirPath);
                if (pid <= 0) {
                    dumpProcessFiles.clear();
                    File[] cachedFiles = tracesFileDir.listFiles();
                    if (cachedFiles != null) {
                        for (int i = 0; i < cachedFiles.length; i++) {
                            if (cachedFiles[i].isFile()) {
                                cachedFiles[i].delete();
                            }
                        }
                    }
                    return;
                }
                Iterator<File> iterator = dumpProcessFiles.iterator();
                while (iterator.hasNext()) {
                    File f = iterator.next();
                    if (f != null) {
                        String name = f.getName();
                        if (name.startsWith("trace_" + pid)) {
                            if (f.exists()) {
                                boolean result = f.delete();
                                VSlog.d(TAG, f.getAbsolutePath() + " delete result: " + result);
                            }
                            iterator.remove();
                        }
                    }
                }
            }
        } catch (Exception e) {
            VSlog.w(TAG, "clearCachedTraces Exception: " + e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: clearAllCachedTraces */
    public void lambda$new$0$ANRManagerService() {
        lambda$clearAllMessages$2$ANRManagerService(-1);
    }

    private void clearRedundantTraces(int pid) {
        ListIterator<File> iterator = dumpProcessFiles.listIterator();
        while (iterator.hasNext()) {
            iterator.next();
        }
        int traceCount = 0;
        while (iterator.hasPrevious()) {
            File trace = iterator.previous();
            if (trace != null) {
                String name = trace.getName();
                if (name.startsWith("trace_" + pid) && (traceCount = traceCount + 1) > 3) {
                    if (trace.exists()) {
                        boolean result = trace.delete();
                        VSlog.i(TAG, trace.getAbsolutePath() + " delete result: " + result);
                    }
                    iterator.remove();
                }
            }
        }
    }

    private boolean isJavaProcess(int pid) {
        int[] iArr;
        if (pid <= 0) {
            return false;
        }
        if (mZygotePids == null) {
            String[] arrayOfString = {"zygote64", "zygote"};
            mZygotePids = Process.getPidsForCommands(arrayOfString);
        }
        if (mZygotePids != null) {
            int j = Process.getParentPid(pid);
            for (int n : mZygotePids) {
                if (j == n) {
                    return true;
                }
            }
        }
        VSlog.i(TAG, "pid: " + pid + " is not a Java process");
        return false;
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (DumpUtils.checkDumpAndUsageStatsPermission(this.mContext, TAG, pw)) {
            synchronized (this.mLogs) {
                int size = this.mLogs.size();
                if (size == 0) {
                    pw.println("No logs.");
                }
                for (int i = 0; i < size; i++) {
                    int pid = this.mLogs.keyAt(i);
                    ArrayList logs = this.mLogs.valueAt(i);
                    if (logs != null && logs.size() > 0) {
                        pw.println("Logs for pid: " + pid);
                        for (int j = 0; j < logs.size(); j++) {
                            pw.println((Object) logs.get(j));
                        }
                        pw.println("*****************************************************");
                    } else {
                        pw.println("No logs for pid: " + pid);
                    }
                }
                Iterator<File> iterator = dumpProcessFiles.iterator();
                pw.println("Dump traces path start:");
                while (iterator.hasNext()) {
                    File f = iterator.next();
                    pw.println(f.getAbsolutePath());
                }
                pw.println("Dump traces path end");
            }
        }
    }

    private String formatMessage(String processName, int pid, String msg) {
        long now = System.currentTimeMillis();
        Date date = new Date(now);
        String time = this.mFormat.format(date);
        return time + "  [" + combine(processName, pid) + "]" + msg;
    }

    private String combine(String processName, int pid) {
        return processName + ":" + pid;
    }

    private void insertLogsLocked(String processName, int pid, String msg) {
        ArrayList<String> log = this.mLogs.get(pid);
        if (log == null) {
            Log.i(TAG, "Create new log list for pid: " + pid);
            log = new ArrayList<>();
            this.mLogs.put(pid, log);
        }
        log.add(formatMessage(processName, pid, msg));
        Log.i(TAG, "Add log successfully, pid = " + pid);
        if (log.size() > 10) {
            Log.i(TAG, "Too many logs for pid: " + pid + ", remove oldest one.");
            log.remove(0);
        }
    }

    private String getLogsLocked(int pid) {
        ArrayList<String> log = this.mLogs.get(pid);
        if (log == null || log.size() == 0) {
            Log.i(TAG, "No logs for pid = " + pid);
            return "no messages for pid = " + pid + "\n";
        }
        Log.i(TAG, "Get logs for pid: " + pid + ", return " + log.size());
        StringBuilder sb = new StringBuilder();
        Iterator<String> it = log.iterator();
        while (it.hasNext()) {
            String str = it.next();
            sb.append(str);
            sb.append("\n");
        }
        return sb.toString();
    }

    private void deleteLogsLocked(int pid) {
        ArrayList<String> log = this.mLogs.get(pid);
        if (log != null) {
            Log.i(TAG, "Delete logs for pid: " + pid);
            this.mLogs.remove(pid);
        }
    }
}