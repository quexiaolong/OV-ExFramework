package com.vivo.services.rms;

import android.os.Debug;
import android.os.RemoteException;
import com.android.server.am.VivoFrozenPackageSupervisor;
import com.vivo.common.utils.VLog;
import com.vivo.services.rms.appmng.AppManager;
import com.vivo.statistics.sdk.ArgPack;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import libcore.io.IoUtils;

/* loaded from: classes.dex */
public class AppCmdExecutor {
    private static final int DEFAULT_BUFFER_SIZE = 4096;
    private static final String TAG = "AppThreadCmd";

    public static ArgPack exeAppCmd(int pid, int cmd, ArgPack argPack) throws RemoteException {
        ProcessInfo app;
        if (!isAlive(pid)) {
            VLog.e(TAG, "Process pid: " + pid + " is died");
            return new ArgPack(new Object[]{false});
        } else if (!isIgnoreFrozen(cmd) && (app = AppManager.getInstance().getProcessInfo(pid)) != null && VivoFrozenPackageSupervisor.getInstance().isFrozenPackage(app.mPkgName, app.mUid)) {
            VLog.e(TAG, "Process pid: " + pid + " is frozen");
            return new ArgPack(new Object[]{false});
        } else if (cmd != 0) {
            if (cmd == 1) {
                return new ArgPack(new Object[]{Boolean.valueOf(dumpBacktrace(pid, argPack))});
            }
            return RMAms.getInstance().exeAppCmd(pid, cmd, argPack);
        } else {
            return new ArgPack(new Object[]{Boolean.valueOf(dumpProcNode(pid, argPack))});
        }
    }

    public static boolean isAlive(int pid) {
        if (pid <= 0) {
            return false;
        }
        File file = new File(String.format("/proc/%d", Integer.valueOf(pid)));
        return file.exists();
    }

    private static boolean dumpProcNode(int pid, ArgPack argPack) {
        if (argPack.isEmpty()) {
            return false;
        }
        FileInputStream input = null;
        FileOutputStream output = null;
        try {
            File srcFile = new File((String) argPack.get(0));
            File dstFile = new File((String) argPack.get(1));
            if (dstFile.exists()) {
                input = new FileInputStream(srcFile);
                output = new FileOutputStream(dstFile);
                copyLarge(input, output);
                return true;
            }
            return false;
        } catch (Exception e) {
            VLog.e(TAG, "dumpProcNode failed pid " + pid + " " + e.toString());
            return false;
        } finally {
            IoUtils.closeQuietly(input);
            IoUtils.closeQuietly(output);
        }
    }

    private static boolean dumpBacktrace(int pid, ArgPack argPack) {
        if (argPack.isEmpty()) {
            return false;
        }
        try {
            boolean isNative = ((Boolean) argPack.get(0)).booleanValue();
            File dstFile = new File((String) argPack.get(1));
            int timeoutSecs = ((Integer) argPack.get(2)).intValue();
            if (dstFile.exists()) {
                if (isNative) {
                    Debug.dumpNativeBacktraceToFileTimeout(pid, dstFile.toString(), timeoutSecs);
                } else {
                    Debug.dumpJavaBacktraceToFileTimeout(pid, dstFile.toString(), timeoutSecs);
                }
                return true;
            }
            return false;
        } catch (Exception e) {
            VLog.e(TAG, "dumpBacktrace failed pid " + pid + " " + e.toString());
            return false;
        }
    }

    private static long copyLarge(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[4096];
        long count = 0;
        while (true) {
            int n = input.read(buffer);
            if (-1 != n) {
                output.write(buffer, 0, n);
                count += n;
            } else {
                return count;
            }
        }
    }

    private static boolean isIgnoreFrozen(int cmd) {
        return cmd == 0;
    }
}