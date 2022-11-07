package com.android.server;

import android.os.FileUtils;
import com.android.server.am.EmergencyBroadcastManager;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoOpenFdMonitorImpl implements IVivoOpenFdMonitor {
    private static final String TAG = "VivoOpenFdMonitorImpl";

    public void logcatFdLeaklog() {
        VSlog.i(TAG, "logcat FdLeak log.");
        Thread worker = new Thread("logcatFdLeaklog") { // from class: com.android.server.VivoOpenFdMonitorImpl.1
            @Override // java.lang.Thread, java.lang.Runnable
            public void run() {
                StringBuilder sb = new StringBuilder();
                VivoOpenFdMonitorImpl.this.logcatSystemlog(sb, 10000);
                VivoOpenFdMonitorImpl.this.catKernelLog(sb);
                VSlog.w(VivoOpenFdMonitorImpl.TAG, "logcatFdLeaklog:" + sb.toString());
                try {
                    FileUtils.stringToFile(new File("/data/anr/fdleadklog.txt"), sb.toString());
                } catch (IOException e) {
                    VSlog.e(VivoOpenFdMonitorImpl.TAG, "Error writing.", e);
                }
            }
        };
        worker.start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void logcatSystemlog(StringBuilder sb, int lines) {
        VSlog.i(TAG, "logcat System log begin.");
        InputStreamReader input = null;
        try {
            try {
                try {
                    Process logcat = new ProcessBuilder("/system/bin/timeout", "-k", "15s", "10s", "/system/bin/logcat", "-v", "threadtime", "-b", "events", "-b", "system", "-b", "main", "-t", String.valueOf(lines)).redirectErrorStream(true).start();
                    try {
                        logcat.getOutputStream().close();
                    } catch (IOException e) {
                    }
                    try {
                        logcat.getErrorStream().close();
                    } catch (IOException e2) {
                    }
                    input = new InputStreamReader(logcat.getInputStream());
                    char[] buf = new char[EmergencyBroadcastManager.FLAG_RECEIVER_KEYAPP];
                    while (true) {
                        int num = input.read(buf);
                        if (num > 0) {
                            sb.append(buf, 0, num);
                        } else {
                            input.close();
                            return;
                        }
                    }
                } catch (Throwable th) {
                    if (input != null) {
                        try {
                            input.close();
                        } catch (IOException e3) {
                        }
                    }
                    throw th;
                }
            } catch (Exception e4) {
                VSlog.e(TAG, "Error running logcat", e4);
                if (input != null) {
                    input.close();
                }
            }
        } catch (IOException e5) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void catKernelLog(StringBuilder sb) {
        VSlog.i(TAG, "cat Kernel Log begin.");
        sb.append("--------- beginning of kernel\n");
        InputStreamReader inputKernel = null;
        try {
            try {
                try {
                    Process kernelProc = new ProcessBuilder("/system/bin/dmesg", "-ST").redirectErrorStream(true).start();
                    try {
                        kernelProc.getOutputStream().close();
                    } catch (IOException e) {
                    }
                    try {
                        kernelProc.getErrorStream().close();
                    } catch (IOException e2) {
                    }
                    inputKernel = new InputStreamReader(kernelProc.getInputStream());
                    char[] buf = new char[EmergencyBroadcastManager.FLAG_RECEIVER_KEYAPP];
                    while (true) {
                        int num = inputKernel.read(buf);
                        if (num <= 0) {
                            break;
                        }
                        sb.append(buf, 0, num);
                    }
                    inputKernel.close();
                } catch (Exception e3) {
                    VSlog.e(TAG, "Error running logcat", e3);
                    if (inputKernel != null) {
                        inputKernel.close();
                    }
                }
            } catch (Throwable th) {
                if (inputKernel != null) {
                    try {
                        inputKernel.close();
                    } catch (IOException e4) {
                    }
                }
                throw th;
            }
        } catch (IOException e5) {
        }
    }
}